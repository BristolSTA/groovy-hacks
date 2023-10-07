import socket
import sys

HOST = "52DC6A.localdomain"  # hostname of the projector
PORT = 23 # Port to listen on (non-privileged ports are > 1023)
Commands = {
    "SHUTTER_ON"  : "be:ef:03:06:00:6b:d9:01:00:20:30:01:00",
    "SHUTTER_OFF" : "be:ef:03:06:00:fb:d8:01:00:20:30:00:00",
    "POWER_ON"    : "be:ef:03:06:00:BA:D2:01:00:00:60:01:00",
    "POWER_OFF"   : "be:ef:03:06:00:2A:D3:01:00:00:60:00:00"
    }

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    print("Connecting to server")
    s.connect((HOST, PORT))
    print("Connected")

    command = Commands[sys.argv[1]]
    bytes_arr_internal = [int(thing, 16) for thing in command.split(":")]
    print(bytes_arr_internal)
    bytes_arr = bytes(bytes_arr_internal)

    print(bytes_arr)

    s.sendall(bytes_arr)
    data = s.recv(1024)

print(f"Received {data!r}")
