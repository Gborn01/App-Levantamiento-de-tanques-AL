#!/usr/bin/env python3
"""Genera un PNG de prueba (sin librerías externas) para la prueba de fotos desde la galería."""
import struct
import sys
import zlib

w, h = 640, 480
rows = []
for y in range(h):
    row = bytearray([0])
    for x in range(w):
        inside = 200 < x < 440 and 80 < y < 420
        row += bytes((0, 44, 119) if inside else (230, 240, 250))
    rows.append(bytes(row))


def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)) \
    + chunk(b"IDAT", zlib.compress(b"".join(rows), 9)) + chunk(b"IEND", b"")
open(sys.argv[1], "wb").write(png)
