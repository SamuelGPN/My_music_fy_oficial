from main_processing import get_youtube_download_link, pesquisar_musica, baixar_audio, run_tirar_ruido_ffmpeg


def pesquisar(nome_musica):
    print('Aguarde..carregando')

    lista_musica = pesquisar_musica(f'{nome_musica}')
    print(lista_musica)
    print('pronto')
    return lista_musica

def baixar_musica_temp(url_yout, caminho_arq_final, caminho_pasta, caminhoa_arq_final_formatado):
    print('caminho: ', caminho_arq_final)
    url_audio = get_youtube_download_link(url_yout)
    baixar = baixar_audio(url_audio, caminho_arq_final, caminho_pasta)

    if baixar is not None:
        audio = run_tirar_ruido_ffmpeg(caminho_arq_final, caminhoa_arq_final_formatado)
        return audio
    else:
        return ""


