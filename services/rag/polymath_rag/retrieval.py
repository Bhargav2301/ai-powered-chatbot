import math
import re
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from tokenizers import Tokenizer

from .schema import Document


@dataclass(frozen=True)
class Chunk:
    document: Document
    text: str
    start: int
    end: int


class MiniLM:
    """Official MiniLM ONNX graph; attention-masked mean pooling and L2 normalization."""

    def __init__(self, model_dir: Path):
        import onnxruntime as ort

        options = ort.SessionOptions()
        options.intra_op_num_threads = 2
        options.inter_op_num_threads = 1
        self.session = ort.InferenceSession(str(model_dir / "minilm.onnx"), options,
                                            providers=["CPUExecutionProvider"])
        self.tokenizer = Tokenizer.from_file(str(model_dir / "tokenizer.json"))
        self.tokenizer.enable_truncation(max_length=256)
        self.tokenizer.enable_padding(pad_id=0, pad_token="[PAD]")
        self.splitter = Tokenizer.from_file(str(model_dir / "tokenizer.json"))
        self.splitter.no_padding()
        self.splitter.no_truncation()

    def chunks(self, documents: list[Document]) -> list[Chunk]:
        result = []
        for document in documents:
            offsets = self.splitter.encode(document.text, add_special_tokens=False).offsets
            for index in range(0, len(offsets), 152):
                window = offsets[index:index + 192]
                start, end = window[0][0], window[-1][1]
                result.append(Chunk(document, document.text[start:end], start, end))
                if index + 192 >= len(offsets):
                    break
        if len(result) > 1500:
            raise ValueError("Dataset exceeds the 1,500 chunk limit; split it into smaller datasets")
        return result

    def encode(self, texts: list[str]) -> np.ndarray:
        vectors = []
        for offset in range(0, len(texts), 16):
            encodings = self.tokenizer.encode_batch(texts[offset:offset + 16])
            ids = np.asarray([item.ids for item in encodings], dtype=np.int64)
            masks = np.asarray([item.attention_mask for item in encodings], dtype=np.int64)
            inputs = {"input_ids": ids, "attention_mask": masks, "token_type_ids": np.zeros_like(ids)}
            names = {item.name for item in self.session.get_inputs()}
            output = self.session.run(None, {k: v for k, v in inputs.items() if k in names})[0]
            pooled = (output * masks[..., None]).sum(axis=1) / masks.sum(axis=1, keepdims=True).clip(1)
            pooled /= np.linalg.norm(pooled, axis=1, keepdims=True).clip(1e-12)
            vectors.append(pooled)
        return np.concatenate(vectors) if vectors else np.empty((0, 384), dtype=np.float32)


def words(text: str) -> list[str]:
    return re.findall(r"\w+", text.lower())


def retrieve(question: str, documents: list[Document], embedder, limit: int = 4) -> list[Chunk]:
    chunks = embedder.chunks(documents)
    if not chunks:
        return []
    embeddings = embedder.encode([question] + [c.text for c in chunks])
    cosine = embeddings[1:] @ embeddings[0]
    terms = set(words(question))
    bags = [words(c.text) for c in chunks]
    avg_length = sum(map(len, bags)) / len(bags)
    lexical = np.zeros(len(chunks))
    for term in terms:
        frequency = sum(term in bag for bag in bags)
        inverse = math.log(1 + (len(bags) - frequency + .5) / (frequency + .5))
        for i, bag in enumerate(bags):
            count = bag.count(term)
            lexical[i] += inverse * count * 2.2 / (count + 1.2 * (.25 + .75 * len(bag) / max(1, avg_length)))
    semantic_order = np.argsort(-cosine, kind="stable")
    lexical_order = np.argsort(-lexical, kind="stable")
    scores = np.zeros(len(chunks))
    for order, values in [(semantic_order, cosine), (lexical_order, lexical)]:
        for rank, index in enumerate(order[:20], 1):
            if values[index] > 0:
                scores[index] += 1 / (60 + rank)
    # Calibratable relevance gate; not a factuality guarantee.
    chosen = [int(i) for i in np.argsort(-scores, kind="stable") if cosine[i] >= .24]
    result = []
    for index in chosen:
        candidate = chunks[index]
        if any(c.document.id == candidate.document.id and abs(c.start - candidate.start) < 100 for c in result):
            continue
        result.append(candidate)
        if len(result) == limit:
            break
    return result
