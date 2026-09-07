from typing import Literal
from urllib.parse import urlsplit

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class SourceImage(StrictModel):
    url: str = Field(max_length=2048)
    alt: str = Field(default="Source illustration", max_length=500)
    caption: str = Field(default="", max_length=1000)

    @field_validator("url")
    @classmethod
    def https_url(cls, value):
        parts = urlsplit(value)
        if parts.scheme != "https" or not parts.hostname or parts.username or parts.password:
            raise ValueError("Images must use HTTPS without credentials")
        return value


class Document(StrictModel):
    id: str = Field(min_length=1, max_length=150)
    dataset_id: str = Field(min_length=1, max_length=100)
    revision: int = Field(ge=1)
    title: str = Field(min_length=1, max_length=200)
    text: str = Field(min_length=1, max_length=20000)
    source_url: str | None = Field(default=None, max_length=2048)
    images: list[SourceImage] = Field(default_factory=list, max_length=6)

    @field_validator("source_url")
    @classmethod
    def validate_source(cls, value):
        if value is not None:
            return SourceImage.https_url(value)
        return value


class ChatRequest(StrictModel):
    question: str = Field(min_length=1, max_length=2000)
    dataset_ids: list[str] = Field(min_length=1, max_length=12)
    documents: list[Document] = Field(max_length=150)
    # Only previous user questions help resolve a follow-up. Prior answers are not evidence.
    previous_questions: list[str] = Field(default_factory=list, max_length=3)

    @field_validator("previous_questions")
    @classmethod
    def bound_history(cls, values):
        if any(len(value) > 2000 for value in values):
            raise ValueError("Previous question is too long")
        return values

    @model_validator(mode="after")
    def boundaries(self):
        if any(d.dataset_id not in self.dataset_ids for d in self.documents):
            raise ValueError("Document is outside the selected datasets")
        if len({d.id for d in self.documents}) != len(self.documents):
            raise ValueError("Duplicate document IDs")
        if sum(len(d.text) + len(d.title) for d in self.documents) > 300000:
            raise ValueError("Select a smaller dataset (300,000 character limit)")
        return self


class Citation(StrictModel):
    id: str
    document_id: str
    dataset_id: str
    revision: int
    title: str
    excerpt: str
    start: int
    end: int
    source_url: str | None
    images: list[SourceImage]


class ChatResponse(StrictModel):
    status: Literal["answered", "insufficient_evidence", "unverified"]
    answer: str
    citations: list[Citation] = Field(default_factory=list)
    model: str
    embedding_model: str = "sentence-transformers/all-MiniLM-L6-v2"
