import requests
import sys


def get_token(username, password):
    url = f"https://oauth.vk.com/token?grant_type=password&client_id=2274003&client_secret=hHbZxrka2uZ6jB1inYsH&" \
        f"username={username}&password={password}"

    response = requests.get(url, allow_redirects=True)
    try:
        return response.json()['access_token']
    except:
        print(response.json())
    exit(0)


def get_url(token):
    url = f"https://api.vk.com/method/execute.resolveScreenName?" \
        f"access_token={token}&v=5.55&screen_name=app6915965_-137565779&owner_id=-137565779&func_v=3"

    response = requests.get(url, allow_redirects=True)
    return response.json()['response']['embedded_uri']['view_url']


token = get_token(sys.argv[1], sys.argv[2])

print(get_url(token))


# accs = [

# ]
#
# tokens = []
# urls = []
#
# for account in accs:
#     token = get_token(*account)
#     print(token)
#     tokens.append(token)
#     url = get_url(token)
#     urls.append(url)
#     print(url)
#
# for token in tokens:
#     print(token)
#
# for url in urls:
#     print(url)
