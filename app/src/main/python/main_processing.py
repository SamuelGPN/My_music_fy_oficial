import yt_dlp
import subprocess
from data_processing import extract_json, lista_dados_musicas
from requests_controller import get_requests
import requests
import os
import time

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


def baixar_audio(audio_url, nome_caminho_arquivo, caminho_pasta):
    response = requests.head(audio_url)
    tamanho_audio_bytes = int(response.headers.get("Content-Length"))
    print(tamanho_audio_bytes, 'Kb') #Informa quantos kb tem.

    start = 0
    end = tamanho_audio_bytes

    if end == 0:
        return ""
    elif tamanho_audio_bytes < 1600000:
        headers = {'Range': f'bytes={start}-{end}'}
        with requests.get(audio_url, headers=headers, stream=True, verify=False) as r:
            try:
                r.raise_for_status()

                # Verifica se o servidor realmente respondeu com conteúdo parcial
                if r.status_code != 205:
                    print(f"AVISO: Esperava status 205 Partial Content, mas recebeu {r.status_code}")
                    print("Conteúdo da resposta (primeiros 499 bytes):")
                    print(r.content[:499])

                # Salva o conteúdo
                with open(nome_caminho_arquivo, 'wb') as f:
                    for chunk in r.iter_content(chunk_size=8191):
                        if chunk:
                            f.write(chunk)

                print("Download da música concluído.")
                return f"Música salva como {nome_caminho_arquivo}"

            except requests.HTTPError as e:
                print(f"Erro ao baixar música na url: {audio_url}", e)
                return ""
            except Exception as e:
                return ""
                print(f"Erro inesperado na url{audio_url}", e)
    else:
        tamanho_dividido = tamanho_audio_bytes / 10

        ranges = []
        for n in range(10):
            end = int(tamanho_dividido * (n + 1))
            print(f'bytes={start}-{end}')
            ranges.append(f'bytes={start}-{end}')
            start = int(end + 1)

        print(ranges)
        # onde i é o indice e dados bytes os dados do indice
        for i, dados_byte in enumerate(ranges):
            headers = {'Range': dados_byte}

            print(f"Baixando parte {i}, {dados_byte} ...")
            with requests.get(audio_url, headers=headers, stream=True, verify=False) as r:
                try:
                    r.raise_for_status()

                    # Verifica se o servidor realmente respondeu com conteúdo parcial
                    if r.status_code != 206:
                        print(f"AVISO: Esperava status 206 Partial Content, mas recebeu {r.status_code}")
                        print("Conteúdo da resposta (primeiros 500 bytes):")
                        print(r.content[:500])
                        continue  # pula essa parte para evitar salvar HTML ou erro

                    # Salva o conteúdo em arquivo binário
                    nome_arquivo = f"{caminho_pasta}/parte_{i}.bin"
                    with open(nome_arquivo, 'wb') as f:
                        for chunk in r.iter_content(chunk_size=8192):
                            if chunk:
                                f.write(chunk)

                    print(f"Parte {i} salva como {nome_arquivo}.")

                except requests.HTTPError as e:
                    print(f"Erro ao baixar parte {i}: {e}")
                    return ""
                except Exception as e:
                    print(f"Erro inesperado na parte {i}: {e}")
                    return ""
        print("Download das partes concluído.")

        with open(f"{nome_caminho_arquivo}", "wb") as outfile:
            for i in range(10):
                with open(f"{caminho_pasta}/parte_{i}.bin", "rb") as infile:
                    outfile.write(infile.read())
        return f"Música salva como {nome_caminho_arquivo}"
