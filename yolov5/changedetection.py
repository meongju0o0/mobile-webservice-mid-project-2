import os
import cv2
import pathlib
import requests
from datetime import datetime
from collections import Counter


class ChangeDetection:
    result_prev = []
    HOST = 'https://meongju0o0.pythonanywhere.com/'
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

    def add(self, names, detected_current, save_dir, image, labels_to_draw):
        """
        Update detected objects and send data if new detections are found.
        """
        self.title = ''
        self.text = ''
        change_flag = False

        # 탐지된 객체 이름 리스트 생성 (labels_to_draw는 (label, bounding_box) 형식으로 전달됨)
        detected_labels = [label for label, _ in labels_to_draw]

        # 객체 카운트 계산
        detected_counts = Counter(detected_labels)

        # 새롭게 탐지된 객체 확인
        for i in range(len(self.result_prev)):
            if self.result_prev[i] == 0 and detected_current[i] == 1:
                change_flag = True
                self.title += f"{names[i]} | "

        self.result_prev = detected_current[:]

        if change_flag:
            # 탐지된 객체와 개수를 본문에 추가
            self.text += "Detected objects:\n"
            self.text += ", ".join([f"{obj} ({count})" for obj, count in detected_counts.items()])
            self.send(save_dir, image, labels_to_draw)

    def send(self, save_dir, image, labels_to_draw):
        """
        Save the image and send data to the server.
        """
        now = datetime.now()
        today = now.date()

        save_path = pathlib.Path(save_dir) / 'detected' / str(today.year) / str(today.month) / str(today.day)
        save_path.mkdir(parents=True, exist_ok=True)

        file_name = f"{now.strftime('%H-%M-%S')}-{now.microsecond}.jpg"
        full_path = save_path / file_name

        # 이미지에 레이블 추가
        for label, (x1, y1, x2, y2) in labels_to_draw:
            cv2.rectangle(image, (int(x1), int(y1)), (int(x2), int(y2)), (255, 0, 0), 2)
            cv2.putText(image, label, (int(x1), int(y1) - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 0, 0), 2)

        # Save resized image
        dst = cv2.resize(image, dsize=(320, 240), interpolation=cv2.INTER_AREA)
        cv2.imwrite(str(full_path), dst)

        # 콘솔에서 추가 title, text 입력 받기
        additional_title = input("Enter additional title: ").strip()
        additional_text = input("Enter additional text: ").strip()
        
        # 기존 title과 text에 추가 title, text 연결
        self.title = self.title.strip(" | ")  # 마지막 ' | ' 제거
        self.title += f" | {additional_title}" if additional_title else ""
        self.text = self.text.strip(", ")  # 마지막 ', ' 제거
        self.text += f" {additional_text}" if additional_text else ""

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
