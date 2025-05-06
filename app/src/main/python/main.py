#import threading

from main_processing import get_youtube_download_link, pesquisar_musica, baixar_audio


def main(caminho, nome_musica):
    print('aaa\n')

    lista_musica = pesquisar_musica(f'{nome_musica}', caminho)
    ordem_list = 0
    url_base = lista_musica[ordem_list].get('url')

    print(lista_musica[ordem_list].get('titulo'))
    print(lista_musica[ordem_list].get('url'))

    audio_url = get_youtube_download_link(url_base)

    if audio_url:
        print("Link de áudio:", audio_url)
    else:
        print('Áudio não encontrado')

    #arquivo = f"{caminho}/{lista_musica[ordem_list].get('titulo')}.mp3"
    arquivo = caminho
    print('caminho: ',caminho)

    # Cria uma thread para baixar
    #thread_download = threading.Thread(target=baixar_audio, args=(audio_url, arquivo))

    # Começa a baixar
    #thread_download.start()
    print('caminho: ',caminho)
    # Espera um pouco pra acumular uns dados (opcional)
    import time
    time.sleep(2)
    print('Aguarde..carregando')
    #time.sleep(12)
    print('pronto')
    # Começa a tocar
    #tocar_audio(arquivo)

    # Espera o download acabar
    #thread_download.join()

    return audio_url


#if __name__ == '__main__':
#    main()