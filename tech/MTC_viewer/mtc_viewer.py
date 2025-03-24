#!/usr/bin/env python3

import click
import mido
import tkinter as tk
import threading
import time
from timecode import Timecode




def mtc_decode(mtc_bytes):
  rhh, mins, secs, frs = mtc_bytes
  rateflag = rhh >> 5
  hrs = rhh & 31
  fps = ['24', '25', '29.97', '30'][rateflag]
  total_frames = int(frs + float(fps) * (secs + mins * 60 + hrs * 60 * 60))
  # total frames must always be an integer above zero
  if total_frames < 1:
    total_frames = 1
  return Timecode(fps, frames=total_frames)


def mtc_decode_quarter_frames(frame_pieces):
  mtc_bytes = bytearray(4)
  if len(frame_pieces) < 8:
    return None
  for piece in range(8):
    mtc_index = 3 - piece//2    # quarter frame pieces are in reverse order of mtc_encode
    this_frame = frame_pieces[piece]
    if this_frame is bytearray or this_frame is list:
      this_frame = this_frame[1]
    data = this_frame & 15      # ignore the frame_piece marker bits
    if piece % 2 == 0:
      # 'even' pieces came from the low nibble
      # and the first piece is 0, so it's even
      mtc_bytes[mtc_index] += data
    else:
      # 'odd' pieces came from the high nibble
      mtc_bytes[mtc_index] += data * 16
  return mtc_decode(mtc_bytes)



# Create a global accumulator for quarter_frames
quarter_frames = [0, 0, 0, 0, 0, 0, 0, 0]

# Initialize Tkinter window
root = tk.Tk()
root.title("MIDI Timecode Display")
root.configure(bg="white")  # Window background color

frame = tk.Frame(root, bg="black", padx=2, pady=2)  # Border around the label
frame.pack(padx=20, pady=20)

timecode_var = tk.StringVar(value="00:00:00:00")  # Default timecode
# Adding black border around the text label
timecode_label = tk.Label(frame, textvariable=timecode_var, font=("Helvetica", 24), bg="red", fg="white", padx=20, pady=10, bd=5, relief="solid", borderwidth=0.2)
timecode_label.pack()

# Dropdown for MIDI port selection
port_var = tk.StringVar()
available_ports = mido.get_input_names()
port_var.set(available_ports[0] if available_ports else "No MIDI ports available")

current_port = None
port_lock = threading.Lock()
last_timecode_time = time.time()
fps = 30  # Default FPS; adjust as needed
frame_interval = 1 / fps  # Expected time between frames
frame_tolerance = frame_interval * 1.5  # 50% tolerance

def update_timecode(tc):
    """Update the Tkinter label with the latest timecode and change background to green."""
    global last_timecode_time
    last_timecode_time = time.time()
    timecode_var.set(tc)
    timecode_label.configure(bg="#90EE90")
    root.update_idletasks()

def handle_message(message):
    if message.type == 'quarter_frame':
        quarter_frames[message.frame_type] = message.frame_value
        if message.frame_type == 7:
            tc = mtc_decode_quarter_frames(quarter_frames)
            update_timecode(f'{tc}')
    elif message.type == 'sysex':
        if len(message.data) == 8 and message.data[0:4] == (127, 127, 1, 1):
            data = message.data[4:]
            tc = mtc_decode(data)
            update_timecode(f'{tc}')
    else:
        print(message)

def listen(port_name):
    global current_port
    with port_lock:
        if current_port:
            current_port.close()
        current_port = mido.open_input(port_name)
    print(f'Listening to MIDI messages on > {port_name} <')
    while True:
        msg = current_port.receive(block=True)
        handle_message(msg)

def check_timeout():
    """Check if no timecode has been received within the expected frame interval with tolerance and change background to red."""
    global last_timecode_time
    if time.time() - last_timecode_time > frame_tolerance:
        timecode_label.configure(bg="red")
    root.after(int(frame_interval * 1000), check_timeout)

check_timeout()

def on_port_selected(*args):
    selected_port = port_var.get()
    if selected_port and selected_port != "No MIDI ports available":
        threading.Thread(target=listen, args=(selected_port,), daemon=True).start()

port_var.trace_add("write", on_port_selected)
port_dropdown = tk.OptionMenu(root, port_var, *available_ports)
port_dropdown.pack(pady=10)

root.mainloop()