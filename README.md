# 👥 𝗠𝗶𝗱𝗣𝗼𝗶𝗻𝘁-𝗕𝗮𝗰𝗸𝗲𝗻𝗱 👥

> [MidPoint 프로젝트 설명 바로 가기](https://github.com/Solucitation)

</br>

## ✨Main 기능
- 회원가입/로그인
- 목적에 맞는 중간지점 찾기
- 커뮤니티 게시판
- 즐겨찾기 장소/친구

</br>

## 👩‍💻 역할 분담
| 이름   | 프로필                                                              | 역할분담 |
| ------ | ------------------------------------------------------------------- | ------- |
| 노경희 | <img src="https://github.com/user-attachments/assets/f00d127b-695e-48e0-9f94-c21a87582dcc" width="100"/> | 회원가입/로그인, api 명세서 |
| 문서현 | <img src="https://github.com/user-attachments/assets/494b790b-7d8f-4aff-b86a-8f641f08cb29" width="100"/> | 목적에 맞는 중간지점 찾기, 즐겨찾기 장소/친구 |
| 최연재 | <img src="https://github.com/user-attachments/assets/3edd11dd-ef2d-46c0-9c5f-5ade1b3b7d75" width="100"/> | 커뮤니티 게시판, erd 설계 |

</br>

## 🌳 프로젝트 구조
```
├─java
│  └─com
│      └─solucitation
│          └─midpoint_backend
│              ├─domain
│              │  ├─community_board
│              │  │  ├─api
│              │  │  ├─dto
│              │  │  ├─entity
│              │  │  ├─repository
│              │  │  └─service
│              │  ├─email
│              │  │  ├─api
│              │  │  ├─dto
│              │  │  └─service
│              │  ├─FavFriend
│              │  │  ├─api
│              │  │  ├─dto
│              │  │  ├─entity
│              │  │  ├─repository
│              │  │  └─service
│              │  ├─FavPlace
│              │  │  ├─api
│              │  │  ├─dto
│              │  │  ├─entity
│              │  │  ├─repository
│              │  │  ├─service
│              │  │  └─validation
│              │  ├─file
│              │  │  ├─controller
│              │  │  └─service
│              │  ├─history2
│              │  │  ├─api
│              │  │  ├─dto
│              │  │  ├─entity
│              │  │  ├─repository
│              │  │  └─service
│              │  ├─logic
│              │  ├─member
│              │  │  ├─api
│              │  │  ├─dto
│              │  │  ├─entity
│              │  │  ├─exception
│              │  │  ├─repository
│              │  │  └─service
│              │  ├─places
│              │  └─reviews
│              └─global
│                  ├─api
│                  ├─auth
│                  ├─config
│                  └─exception
└─resources
    ├─templates
    └─application.properties
```
