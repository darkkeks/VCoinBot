import requests
import time


def get_token(username, password):
    url = f"https://oauth.vk.com/token?grant_type=password&client_id=2274003&client_secret=hHbZxrka2uZ6jB1inYsH&" \
        f"username={username}&password={password}"

    response = requests.get(url, allow_redirects=True)
    try:
        return response.json()['access_token']
    except:
        print(response.json())
        return None


def get_url(token):
    url = f"https://api.vk.com/method/execute.resolveScreenName?" \
        f"access_token={token}&v=5.55&screen_name=app6915965_-137565779&owner_id=-137565779&func_v=3"

    response = requests.get(url, allow_redirects=True)
    try:
        return response.json()['response']['embedded_uri']['view_url']
    except:
        print(response.json())
        return None


# token = get_token(sys.argv[1], sys.argv[2])
#
# print(token)
# print(get_url(token))
#
# exit(0)

accs = [
]

for account in accs:
    account = account.split(':')
    print(account)
    token = get_token(*account)
    if token is not None:
        url = get_url(token)
        print(url)
    print()
    time.sleep(2)
