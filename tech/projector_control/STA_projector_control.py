import requests
import sys

#Control for the STA's P502HL NEC projectors
#Probably run on qlab so run "filename.py COMMAND" to run the command

hostname = "pj-7340050eg.localdomain"  # hostname of the projector

#The hostnames for the STA projectors should be sta-1.localdomain or sta-2.localdomain
#This should be written on the projectors assuming I've got aroun to it

address = "http://" + hostname + ":3060/?D="


try:
    command = sys.argv[1]
except IndexError:
    command = input()


if command == "SHUTTER_ON" or command == "1":
    requests.get(address + "%05%02%10%00%00%00") #AV Mute
    
elif command == "SHUTTER_OFF" or command == "2":
    requests.get(address + "%05%02%11%00%00%00") #AV UnMute
    
elif command == "POWER_ON" or command == "3":
    requests.get(address + "%05%02%00%00%00%00") #Power On
    requests.get(address + "%07%02%03%00%00%02%01%BF") #Set Input to HDBaseT
    
elif command == "POWER_OFF" or command == "4":
    requests.get(address + "%05%02%01%00%00%00") #Power Off