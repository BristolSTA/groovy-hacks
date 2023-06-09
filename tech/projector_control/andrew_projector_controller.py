import socket
from enum import Enum 

HOST = "192.168.0.111"  # Standard loopback interface address (localhost)
PORT = 23 # Port to listen on (non-privileged ports are > 1023)

class Command(str, Enum):
    SHUTTER_ON = "be:ef:03:06:00:6b:d9:01:00:20:30:01:00"
    SHUTTER_OFF = "be:ef:03:06:00:fb:d8:01:00:20:30:00:00"

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    print("Connecting to server")
    s.connect((HOST, PORT))
    print("Connected")

    command = Command.SHUTTER_OFF
    bytes_arr_internal = [int(thing, 16) for thing in command.split(":")]
    print(bytes_arr_internal)
    bytes_arr = bytes(bytes_arr_internal)

    print(bytes_arr)

    s.sendall(bytes_arr)
    data = s.recv(1024)

print(f"Received {data!r}")
