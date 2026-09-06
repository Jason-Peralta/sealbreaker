"""A minimal NBT reader and writer (gzip-compressed root compound), enough for structure templates and level.dat.

Values are plain Python: dict = compound, list = list (homogeneous), and typed scalars through the tag classes
below so the writer knows which tag to emit. Run any of the tools that import it from the repository root.
"""
import gzip
import struct

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE = 0, 1, 2, 3, 4, 5, 6
TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, TAG_LONG_ARRAY = 7, 8, 9, 10, 11, 12


class Byte(int):
    tag = TAG_BYTE


class Short(int):
    tag = TAG_SHORT


class Int(int):
    tag = TAG_INT


class Long(int):
    tag = TAG_LONG


class Float(float):
    tag = TAG_FLOAT


class Double(float):
    tag = TAG_DOUBLE


class List(list):
    """A homogeneous list; pass the element tag for an empty list."""

    def __init__(self, items=(), element_tag=None):
        super().__init__(items)
        self.element_tag = element_tag


def tag_of(value):
    if hasattr(value, "tag"):
        return value.tag
    if isinstance(value, bool):
        return TAG_BYTE
    if isinstance(value, int):
        return TAG_INT
    if isinstance(value, float):
        return TAG_DOUBLE
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, dict):
        return TAG_COMPOUND
    if isinstance(value, list):
        return TAG_LIST
    raise TypeError(f"no NBT tag for {type(value)}")


def _write_payload(out, tag, value):
    if tag == TAG_BYTE:
        out.append(struct.pack(">b", int(value)))
    elif tag == TAG_SHORT:
        out.append(struct.pack(">h", int(value)))
    elif tag == TAG_INT:
        out.append(struct.pack(">i", int(value)))
    elif tag == TAG_LONG:
        out.append(struct.pack(">q", int(value)))
    elif tag == TAG_FLOAT:
        out.append(struct.pack(">f", float(value)))
    elif tag == TAG_DOUBLE:
        out.append(struct.pack(">d", float(value)))
    elif tag == TAG_STRING:
        data = value.encode("utf-8")
        out.append(struct.pack(">H", len(data)))
        out.append(data)
    elif tag == TAG_LIST:
        element = value.element_tag if isinstance(value, List) and value.element_tag is not None else (tag_of(value[0]) if value else TAG_END)
        out.append(struct.pack(">bi", element, len(value)))
        for item in value:
            _write_payload(out, element, item)
    elif tag == TAG_COMPOUND:
        for key, item in value.items():
            item_tag = tag_of(item)
            out.append(struct.pack(">b", item_tag))
            data = key.encode("utf-8")
            out.append(struct.pack(">H", len(data)))
            out.append(data)
            _write_payload(out, item_tag, item)
        out.append(b"\x00")
    else:
        raise TypeError(f"unsupported tag {tag}")


def dumps(root, name=""):
    out = [struct.pack(">b", TAG_COMPOUND)]
    data = name.encode("utf-8")
    out.append(struct.pack(">H", len(data)))
    out.append(data)
    _write_payload(out, TAG_COMPOUND, root)
    return b"".join(out)


def write(path, root, name=""):
    with gzip.open(path, "wb") as f:
        f.write(dumps(root, name))


class _Reader:
    def __init__(self, data):
        self.data = data
        self.pos = 0

    def take(self, fmt):
        size = struct.calcsize(fmt)
        value = struct.unpack(fmt, self.data[self.pos:self.pos + size])
        self.pos += size
        return value[0] if len(value) == 1 else value

    def string(self):
        length = self.take(">H")
        value = self.data[self.pos:self.pos + length].decode("utf-8", "replace")
        self.pos += length
        return value

    def payload(self, tag):
        if tag == TAG_BYTE:
            return Byte(self.take(">b"))
        if tag == TAG_SHORT:
            return Short(self.take(">h"))
        if tag == TAG_INT:
            return Int(self.take(">i"))
        if tag == TAG_LONG:
            return Long(self.take(">q"))
        if tag == TAG_FLOAT:
            return Float(self.take(">f"))
        if tag == TAG_DOUBLE:
            return Double(self.take(">d"))
        if tag == TAG_BYTE_ARRAY:
            n = self.take(">i")
            value = list(self.data[self.pos:self.pos + n])
            self.pos += n
            return value
        if tag == TAG_STRING:
            return self.string()
        if tag == TAG_LIST:
            element, n = self.take(">bi")
            return List([self.payload(element) for _ in range(n)], element)
        if tag == TAG_COMPOUND:
            result = {}
            while True:
                item_tag = self.take(">b")
                if item_tag == TAG_END:
                    return result
                key = self.string()
                result[key] = self.payload(item_tag)
        if tag == TAG_INT_ARRAY:
            n = self.take(">i")
            return [self.take(">i") for _ in range(n)]
        if tag == TAG_LONG_ARRAY:
            n = self.take(">i")
            return [self.take(">q") for _ in range(n)]
        raise TypeError(f"unsupported tag {tag}")


def loads(data):
    reader = _Reader(data)
    tag = reader.take(">b")
    name = reader.string()
    return name, reader.payload(tag)


def read(path):
    with open(path, "rb") as f:
        data = f.read()
    if data[:2] == b"\x1f\x8b":
        data = gzip.decompress(data)
    return loads(data)
