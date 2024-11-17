import os
import cv2
import pathlib
import requests
from datetime import datetime

class ChangeDetection:
    result_prev = []
    HOST = 'http://127.0.0.1:8000'
    username = 'admin'
    password = 'apdpfhd3!'
    token = 'dc2370468c357d774045c432acd5936362161aa7'
    author = 1
    title = 'yolo_test'
    text = 'Lorem reprehenderit aliquip ullamco qui duis nulla ipsum consectetur.'

    def __init__(self, names):
        self.result_prev = [0 for _ in range(len(names))]

        res = requests.post(self.HOST + '/api-token-auth/', {
            'username': self.username,
            'password': self.password
        })

        res.raise_for_status()
        self.token = res.json()['token']
        print(self.token)
    
    def add(self, names, detected_current, save_dir, image):
        self.title = ''
        self.text = ''
        change_flag = False


        for i in range(len(self.result_prev)):
            if self.result_prev[i] == 0 and detected_current[i] == 1:
                change_flag = True
                self.title = names[i]
                self.text += f"{names[i]},"

        self.result_prev = detected_current[:]

        if change_flag:
            self.send(save_dir, image)
    
    def send(self, save_dir, image):
        now = datetime.now()
        today = now.date()

        save_path = pathlib.Path(save_dir) / 'detected' / str(today.year) / str(today.month) / str(today.day)
        save_path.mkdir(parents=True, exist_ok=True)

        file_name = f"{now.strftime('%H-%M-%S')}-{now.microsecond}.jpg"
        full_path = save_path / file_name

        # Save resized image
        dst = cv2.resize(image, dsize=(320, 240), interpolation=cv2.INTER_AREA)
        cv2.imwrite(str(full_path), dst)

        headers = {
            'Authorization': f'JWT {self.token}',
            'Accept': 'application/json'
        }

        data = {
            'title': self.title,
            'text': self.text,
            'author': self.author,
            'created_date': now.isoformat(),
            'published_date': now.isoformat()
        }

        files = {'image': open(full_path, 'rb')}
        try:
            res = requests.post(f'{self.HOST}/api_root/Post/', data=data, files=files, headers=headers)
            print(f"Response: {res.status_code}, {res.text}")
        except requests.RequestException as e:
            print(f"Failed to send data: {e}")