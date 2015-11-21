import socket
import sys
import select
import Queue
import subprocess

PORT = 8888

def promp_me(msg) :
    #sys.stdout.write('<You> ')
    #sys.stdout.flush()
    print "<You>: " + msg
def promp_him(msg):
    #sys.stdout.write('\t\t<Him> ')
    #sys.stdout.flush()
    print "\t\t\t<Him>: " + msg
def servidor():
    
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.bind(('',PORT))
    s.settimeout(2)
    while True:
        socket_list = [sys.stdin, s]
        read_sockets, write_sockets, error_sockets = select.select(socket_list , [], [])
        for sock in read_sockets:
            #incoming message from remote server
            if sock == s:
                data, addr = sock.recvfrom(1024)
                if not data :
                    print '\nDisconnected from chat server'
                    sys.exit()
                else :
                    promp_him(data)
                    if data == "GPS":
                        s.sendto("GPS!54.322;32.123;2.3;4.5;",addr)
                        print "\nGPS!54.322;32.123;2.3;4.5;\n"
                    elif data == "Weather":
                        s.sendto("Weather!54.322;32.123;091123;400.3",addr)
                        print "\nWeather!54.322;32.123;091123;400.3\n"
                    elif data == "Stop":
                        s.sendto("OK",addr)
                        print "\nOK\n"
                    elif data == "connect":
                        print "alive"
                        s.sendto("alive\n",addr)
                    

if __name__ == "__main__":
    
    print "------ Welcome to the arduino emulator------"
    print "This program simulates the arduino in the sense that answers the messages that the smartphone app sends"
    print "\n"
    IP = subprocess.Popen('ifconfig wlan0 | grep "inet addr"',shell=True,stdout=subprocess.PIPE,)
    out = IP.communicate()[0]
    if out != "":
        print "wlan0 " + out
    IP = subprocess.Popen('ifconfig eth0 | grep "inet addr"',shell=True,stdout=subprocess.PIPE,)
    out = IP.communicate()[0]
    if out != "":
        print "eth0 " + out
    
    servidor()
    


