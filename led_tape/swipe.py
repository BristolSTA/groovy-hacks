import sys
from enum import Enum
from PIL import Image, ImageDraw

WIDTH = 100
FRAME_RATE = WIDTH


class Color(Enum):
    Black = (0, 0, 0)
    White = (255, 0, 0)


class Loader:
    def __init__(self):
        self.current_x = 0
        self.len = 50

    def step(self):
        self.current_x += 1

    def draw(self, image):
        for i in range(100):
            if i - self.current_x > 0:
                brightness = round(250 / (5 * (i - self.current_x)))
            else:
                brightness = 0
            image.putpixel((i, 0), (brightness, brightness, brightness))


def create_next_frame(loader, frame):
    image = Image.new(mode="RGB", size=(WIDTH, 1))
    loader.step()
    print(f"{loader.current_x=}")
    loader.draw(image)
    image.save(f"output/output_{frame:04d}.png")


def main():
    loader = Loader()

    for i in range(100):
        create_next_frame(loader, i)


if __name__ == "__main__":
    main()
