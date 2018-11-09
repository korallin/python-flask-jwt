FROM python:2

WORKDIR /usr/src/app

RUN apt-get update && apt-get install -y python-dev libldap2-dev libsasl2-dev libssl-dev

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

CMD [ "python", "./app/app.py" ]
