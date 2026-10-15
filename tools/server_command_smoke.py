#!/usr/bin/env python3
"""Exercise EnhancedTimer on an isolated localhost test server through RCON."""
import argparse
import os
import socket
import struct

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--port', type=int, default=25577)
parser.add_argument('--stop', action='store_true', help='Stop the test server after the checks')
args = parser.parse_args()

with socket.create_connection(('127.0.0.1', args.port), timeout=10) as connection:
    def read_exact(size):
        data = b''
        while len(data) < size:
            piece = connection.recv(size - len(data))
            if not piece:
                raise RuntimeError('RCON connection closed')
            data += piece
        return data

    def packet(request_id, kind, text):
        body = struct.pack('<ii', request_id, kind) + text.encode() + b'\0\0'
        connection.sendall(struct.pack('<i', len(body)) + body)
        size = struct.unpack('<i', read_exact(4))[0]
        assert 10 <= size <= 1_000_000, 'Invalid RCON response size'
        response = read_exact(size)
        return struct.unpack('<i', response[:4])[0], response[8:-2].decode()

    password = os.environ.get('ET_SMOKE_RCON_PASSWORD', 'EnhancedTimerLocalSmokeOnly')
    assert packet(1, 3, password)[0] == 1, 'Authentication failed'
    checks = [
        ('ets countdown 2d', 'RUNNING'), ('ets pause', 'PAUSED'),
        ('ets split Server-Smoke', 'PAUSED'), ('ets splits', 'Server-Smoke'),
        ('ets stop', 'STOPPED'), ('ets undo', 'PAUSED'), ('ets reset', 'READY'),
        ('ets set 2h', 'PAUSED'), ('ets team create smoke', 'team:smoke'),
        ('ets countdown 2h', 'RUNNING'), ('ets rules freeze true', 'team:smoke'),
        ('ets team delete smoke', 'global'),
    ]
    for index, (command, expected) in enumerate(checks, 2):
        _, response = packet(index, 2, command)
        assert expected in response, (command, response)
        print(f'PASS {command}: {response.strip()}')
    if args.stop:
        print(packet(99, 2, 'stop')[1])
