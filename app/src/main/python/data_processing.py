import re
import json
import time


def extract_json(data):
    # Regex para encontrar o JSON dentro da variável ytInitialData
    regex = r'ytInitialData\s*=\s*(\{.*?\});'  # Regex para capturar o JSON
    match = re.search(regex, data, re.DOTALL)

    if match:
        # Convertendo o JSON em um dicionário Python
        yt_initial_data = json.loads(match.group(1))
        #print(yt_initial_data.keys()) #para ver as chaves que tem no JSON
        return yt_initial_data
    else:
        print('ytInitialData não encontrado!')
        return None

def lista_dados_musicas(json_data):
    try:
        itens = json_data["contents"]["twoColumnSearchResultsRenderer"]["primaryContents"]["sectionListRenderer"]["contents"]
        lista_videos = []
        for bloco in itens:
            if "itemSectionRenderer" in bloco:
                for conteudo in bloco["itemSectionRenderer"]["contents"]:
                    if "videoRenderer" in conteudo:
                        video = conteudo["videoRenderer"]
                        video_id = video.get("videoId")
                        titulo = video.get("title", {}).get("runs", [{}])[0].get("text", "")
                        url = f"https://www.youtube.com/watch?v={video_id}"
                        lista_videos.append({"titulo": titulo, "url": url})
                        #print(f"{titulo}: {url}")
        return lista_videos
    except KeyError as e:
        print("Erro ao acessar os dados:", e)
        return None



