"""Verify packaged 16 KB ELF/ZIP alignment without relying on a host readelf binary."""
import argparse
import struct
import zipfile

def verify(path):
    count = 0
    with open(path, "rb") as raw, zipfile.ZipFile(path) as archive:
        for info in archive.infolist():
            if not (info.filename.startswith("lib/") and info.filename.endswith(".so")):
                continue
            data = archive.read(info)
            assert data[:4] == b"\x7fELF" and data[5] == 1, info.filename
            if data[4] == 2:
                offset = struct.unpack_from("<Q", data, 32)[0]
                size, number = struct.unpack_from("<HH", data, 54)
                for i in range(number):
                    header = struct.unpack_from("<IIQQQQQQ", data, offset + i * size)
                    if header[0] == 1:
                        assert header[7] >= 16384 and (header[2] - header[3]) % 16384 == 0, info.filename
            else:
                offset = struct.unpack_from("<I", data, 28)[0]
                size, number = struct.unpack_from("<HH", data, 42)
                for i in range(number):
                    header = struct.unpack_from("<IIIIIIII", data, offset + i * size)
                    if header[0] == 1:
                        assert header[7] >= 16384 and (header[1] - header[2]) % 16384 == 0, info.filename
            if info.compress_type == zipfile.ZIP_STORED:
                raw.seek(info.header_offset + 26)
                name_size, extra_size = struct.unpack("<HH", raw.read(4))
                assert (info.header_offset + 30 + name_size + extra_size) % 16384 == 0, info.filename
            count += 1
    assert count >= 4, "Expected ARM64/x86_64 libraries and 32-bit compatibility stubs"
    print(f"Verified {count} native libraries: 16 KB ELF and uncompressed ZIP alignment")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("apk")
    verify(parser.parse_args().apk)
