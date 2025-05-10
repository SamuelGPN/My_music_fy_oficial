import yt_dlp
import subprocess
from data_processing import extract_json, lista_dados_musicas
from requests_controller import get_requests
import os

def pesquisar_musica(name_music):
    url_pesquisa = f'{name_music}'.replace(' ', '+')
    url = f'https://www.youtube.com/results?search_query={url_pesquisa}'
    data = get_requests(url).text
    #with open(f"{caminho}/dados.html", 'w', encoding='UTF-8') as f:
    #    f.write(data)
    json_data = extract_json(data)
    lista_nome_links = lista_dados_musicas(json_data)
    return lista_nome_links

def get_youtube_download_link(url):
    ydl_opts = {
        'format': 'bestaudio/best',  # pega o melhor formato de áudio disponível
        'extractaudio': True,  # extrai apenas o áudio
        'quiet': True,  # não exibe logs detalhados
        'noplaylist': True,  # não pega playlists, só o vídeo
        'outtmpl': 'downloads/%(id)s.%(ext)s'  # salva o arquivo com o nome do id do vídeo
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(url, download=False)
        # print(info_dict)
        # Pega a URL do áudio
        if 'formats' in info_dict:
            for f in info_dict['formats']:
                # print(f)
                # time.sleep(10000)
                if f['ext'] == 'webm' and f['acodec'] != 'none':  # and 'https://rr1' in f['url'] and f['audio_channels'] != 'none':
                    print('url: ', f['url'])
                    return f['url']
        return None


def baixar_audio(audio_url, nome_arquivo):
    with get_requests(audio_url, stream=True) as r:
        r.raise_for_status()
        with open(nome_arquivo, 'wb') as f:
            for chunk in r.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)
        print("Download finalizado!")
"""
def tocar_audio(nome_arquivo):
    # Usando ffplay (do pacote ffmpeg) para tocar
    subprocess.run(['ffplay', '-nodisp', '-autoexit', nome_arquivo])



def tocar_audio_pygame(nome_arquivo):
    try:
        # Inicializa o mixer do pygame
        pygame.mixer.init()

        # Carrega o arquivo de áudio
        pygame.mixer.music.load('./arq/temp/saida.webm')

        # Começa a tocar o áudio
        pygame.mixer.music.play()

        # Espera o áudio terminar
        while pygame.mixer.music.get_busy():
            pygame.time.Clock().tick(10)  # Espera com um clock para liberar a CPU

    except Exception as e:
        print(f"Erro ao tocar áudio: {e}")
"""
