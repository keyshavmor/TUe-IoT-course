import json
from pprint import pprint

with open('nl_59-xh-jt.json') as f:
    data = json.load(f)
pprint(data)
   
string=data["results"][0]["plate"]

print(string)

f=open("matched.txt","w+")

f.write(string)

f.close()
