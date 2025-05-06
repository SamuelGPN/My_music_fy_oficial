# network.py
import requests

def requisicao_url(url):
    response = requests.get(url)
    if response.status_code == 200:
        return 'Hellowwwwww'  # Retorna a resposta JSON
    else:
        return f"Error orror {response.status_code}: Unable to fetch the URL."
