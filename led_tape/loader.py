import sys
from enum import Enum
from PIL import Image, ImageDraw

WIDTH = 100
FRAME_RATE = WIDTH


class Color(Enum):
    Black = (0, 0, 0)
    White = (255, 0, 0)
    WhiteDim = (120, 0, 0)
    Red = (255, 0, 0)


class Loader:
    def __init__(self):
        self.current_x = 0

    def thing_pos(self, current_frame):
        # Frames till thing
        thing_start_pos = self.current_x + WIDTH
        current_thing_pos = thing_start_pos - (current_frame % 100)

        if current_thing_pos >= 100:
            return None
        return current_thing_pos

    def step(self):
        self.current_x += 1


def create_next_frame(loader, frame):
    image = Image.new(mode="RGB", size=(WIDTH, 1))

    if frame % FRAME_RATE == 0:
        loader.step()

    for i in range(WIDTH):
        if i < loader.current_x:
            image.putpixel((i, 0), Color.Red.value)
        else:
            image.putpixel((i, 0), Color.Black.value)

    if pos := loader.thing_pos(frame):
        image.putpixel((pos, 0), Color.White.value)
        if not pos + 1 > (WIDTH - 1):
            image.putpixel((pos + 1, 0), Color.WhiteDim.value)
        if not pos - 1 < 0:
            image.putpixel((pos - 1, 0), Color.WhiteDim.value)

    image.save(f"output/output_{frame:04d}.png")


def main():
    loader = Loader()

    for i in range(90 * FRAME_RATE):
        create_next_frame(loader, i)


if __name__ == "__main__":
    main()
