from main_processing import get_youtube_download_link, tocar_audio


def test_pegar_url_do_mp3():
    urls = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ'  # exemplo real
    link = get_youtube_download_link(urls)
    assert link is not None
    assert link.startswith("http")
    print(link)

def test_tocar():
    tocar_audio('a')

