from main_processing import get_youtube_download_link, pesquisar_musica


def pesquisar(nome_musica):
    print('Aguarde..carregando')

    lista_musica = pesquisar_musica(f'{nome_musica}')
    print(lista_musica)
    print('pronto')
    return lista_musica

def baixar_musica_temp(url_yout, caminho_pasta):
    print('caminho: ', caminho_pasta)
    while True:
        baixar_audio = get_youtube_download_link(url_yout, caminho_pasta)
        if not baixar_audio:
            print('O arquivo retornou 0 Kb, tentando novamente... ')
        else:
            return baixar_audio


