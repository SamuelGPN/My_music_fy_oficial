#import threading

from main_processing import get_youtube_download_link, pesquisar_musica, baixar_audio


def main(nome_musica):
    print('aaa\n')

    lista_musica = pesquisar_musica(f'{nome_musica}')
    print(lista_musica)

    # Espera um pouco pra acumular uns dados (opcional)
    import time
    time.sleep(2)
    print('Aguarde..carregando')

    print('pronto')
    # Começa a tocar
    #tocar_audio(arquivo)

    # Espera o download acabar
    #thread_download.join()

    return lista_musica


#if __name__ == '__main__':
#    main()