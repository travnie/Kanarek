#!/usr/bin/env python3
"""Tap the Docbench tile from an Android UIAutomator dump in emulator CI."""

import re
import subprocess
import sys
import xml.etree.ElementTree as ET


def docbench_tap_coordinates(xml_path: str) -> tuple[int, int]:
    nodes = ET.parse(xml_path).iter("node")
    match = next(
        (
            node
            for node in nodes
            if "Docbench" in (node.get("text", "") + " " + node.get("content-desc", ""))
        ),
        None,
    )
    if match is None:
        raise ValueError("offline Tools hub did not expose Docbench")
    bounds = [int(value) for value in re.findall(r"\d+", match.get("bounds", ""))]
    if len(bounds) != 4:
        raise ValueError("Docbench target bounds missing")
    return (bounds[0] + bounds[2]) // 2, (bounds[1] + bounds[3]) // 2


if __name__ == "__main__":
    x, y = docbench_tap_coordinates(sys.argv[1])
    subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)], check=True)
