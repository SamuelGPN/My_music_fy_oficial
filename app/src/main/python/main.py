from main_processing import get_youtube_download_link, pesquisar_musica, baixar_audio


def pesquisar(nome_musica):
    print('Aguarde..carregando')

    lista_musica = pesquisar_musica(f'{nome_musica}')
    print(lista_musica)
    print('pronto')
    return lista_musica

def baixar_musica_temp(url_yout, caminho_arq_final, caminho_pasta):
    print('caminho: ', caminho_arq_final)
    url_audio = get_youtube_download_link(url_yout)
    audio = baixar_audio(url_audio, caminho_arq_final, caminho_pasta)

    return audio


