import time

import requests

def post_requests(url, data=None, json=None):
    requests.post(url, data=data, json=json)

def get_requests(url, stream=None):
    return requests.get(url, stream=stream)

def test_post():
    print(get_requests('opening sao'))


