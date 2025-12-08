# API Testing Guide - Vibe Clip

## Базовый URL
```
http://localhost:8000
```

## Важно!
После регистрации/входа сохраните JWT токен из ответа и используйте его в заголовке `Authorization: Bearer <TOKEN>` для всех последующих запросов.

---

## 1. АУТЕНТИФИКАЦИЯ

### 1.1 Регистрация нового пользователя
```bash
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "username": "testuser",
    "password": "password123"
  }'
```

**Ответ:** `{"accessToken": "eyJhbGc...", "tokenType": "Bearer"}`

**Сохраните токен!** Например: `TOKEN="eyJhbGc..."`

---

### 1.2 Вход в систему
```bash
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Ответ:** `{"accessToken": "eyJhbGc...", "tokenType": "Bearer"}`

---

## 2. ПОЛЬЗОВАТЕЛЬ

### 2.1 Получение информации о текущем пользователе
```bash
curl -X GET http://localhost:8000/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## 3. ВИДЕО

### 3.1 Создание видео (требует роль USER или ADMIN)
```bash
curl -X POST http://localhost:8000/api/v1/videos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Мое первое видео",
    "description": "Описание видео",
    "videoUrl": "https://example.com/video.mp4",
    "thumbnailUrl": "https://example.com/thumb.jpg",
    "durationSeconds": 60,
    "hashtags": ["комедия", "юмор", "смех"]
  }'
```

**Сохраните ID видео из ответа!** Например: `VIDEO_ID="<uuid>"`

---

### 3.2 Получение видео по ID
```bash
curl -X GET http://localhost:8000/api/v1/videos/$VIDEO_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3.3 Получение метрик видео
```bash
curl -X GET http://localhost:8000/api/v1/videos/$VIDEO_ID/metrics \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3.4 Обновление видео (только автор)
```bash
curl -X PUT http://localhost:8000/api/v1/videos/$VIDEO_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Обновленный заголовок",
    "description": "Новое описание",
    "hashtags": ["новый", "хэштег"]
  }'
```

---

### 3.5 Публикация видео
```bash
curl -X POST http://localhost:8000/api/v1/videos/$VIDEO_ID/publish \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3.6 Получение списка моих видео
```bash
curl -X GET "http://localhost:8000/api/v1/videos/my?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

С фильтрацией по статусу:
```bash
curl -X GET "http://localhost:8000/api/v1/videos/my?status=PUBLISHED&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3.7 Получение списка опубликованных видео
```bash
curl -X GET "http://localhost:8000/api/v1/videos?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3.8 Удаление видео (только автор)
```bash
curl -X DELETE http://localhost:8000/api/v1/videos/$VIDEO_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## 4. РЕАКЦИИ НА ВИДЕО

### 4.1 Создание реакции на видео (через VideoController)
```bash
curl -X POST http://localhost:8000/api/v1/videos/$VIDEO_ID/reactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reactionType": "LIKE"
  }'
```

---

### 4.2 Создание реакции (через ReactionController)
```bash
curl -X POST http://localhost:8000/api/v1/reactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "videoId": "'$VIDEO_ID'",
    "reactionType": "VIEW",
    "watchDurationSeconds": 45
  }'
```

**Типы реакций:** `LIKE`, `DISLIKE`, `VIEW`, `SHARE`, `REPORT`, `SKIP`

---

### 4.3 Проверка наличия реакции
```bash
curl -X GET "http://localhost:8000/api/v1/reactions/video/$VIDEO_ID/check?reactionType=LIKE" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 4.4 Получение моих реакций на видео
```bash
curl -X GET http://localhost:8000/api/v1/reactions/video/$VIDEO_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

### 4.5 Получение всех моих реакций определенного типа
```bash
curl -X GET "http://localhost:8000/api/v1/reactions/my?reactionType=LIKE" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 4.6 Удаление реакции
```bash
curl -X DELETE "http://localhost:8000/api/v1/reactions/video/$VIDEO_ID?reactionType=LIKE" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 5. ПАПКИ (FOLDERS)

### 5.1 Создание папки
```bash
curl -X POST http://localhost:8000/api/v1/folders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Моя первая папка",
    "description": "Папка для комедийных видео",
    "preference": {
      "allowedHashtags": ["комедия", "юмор"],
      "blockedHashtags": ["политика"],
      "minDurationSeconds": 10,
      "maxDurationSeconds": 60,
      "freshnessWeight": 0.7,
      "popularityWeight": 0.3
    }
  }'
```

**Сохраните ID папки!** Например: `FOLDER_ID="<uuid>"`

---

### 5.2 Получение списка моих папок
```bash
curl -X GET http://localhost:8000/api/v1/folders \
  -H "Authorization: Bearer $TOKEN"
```

---

### 5.3 Получение папки по ID
```bash
curl -X GET http://localhost:8000/api/v1/folders/$FOLDER_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

### 5.4 Обновление папки
```bash
curl -X PUT http://localhost:8000/api/v1/folders/$FOLDER_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Обновленное имя папки",
    "description": "Новое описание",
    "preference": {
      "freshnessWeight": 0.8,
      "popularityWeight": 0.2
    }
  }'
```

---

### 5.5 Получение ленты папки (рекомендации)
```bash
curl -X GET "http://localhost:8000/api/v1/folders/$FOLDER_ID/feed?limit=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 5.6 Перегенерация ленты папки
```bash
curl -X POST "http://localhost:8000/api/v1/folders/$FOLDER_ID/regenerate?limit=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 5.7 Архивирование папки
```bash
curl -X POST http://localhost:8000/api/v1/folders/$FOLDER_ID/archive \
  -H "Authorization: Bearer $TOKEN"
```

---

### 5.8 Удаление папки
```bash
curl -X DELETE http://localhost:8000/api/v1/folders/$FOLDER_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## 6. АДМИН ПАНЕЛЬ (требует роль ADMIN)

### 6.1 Получение видео на модерации
```bash
curl -X GET "http://localhost:8000/api/v1/admin/moderation/videos?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

С фильтрацией по статусу:
```bash
curl -X GET "http://localhost:8000/api/v1/admin/moderation/videos?status=PENDING&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 6.2 Одобрение видео
```bash
curl -X POST http://localhost:8000/api/v1/admin/moderation/videos/$VIDEO_ID/approve \
  -H "Authorization: Bearer $TOKEN"
```

---

### 6.3 Отклонение видео
```bash
curl -X POST http://localhost:8000/api/v1/admin/moderation/videos/$VIDEO_ID/reject \
  -H "Authorization: Bearer $TOKEN"
```

---

### 6.4 Получение видео с репортами
```bash
curl -X GET "http://localhost:8000/api/v1/admin/moderation/videos/reported?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 6.5 Удаление видео администратором
```bash
curl -X DELETE http://localhost:8000/api/v1/admin/videos/$VIDEO_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## ПОЛНЫЙ СЦЕНАРИЙ ИСПОЛЬЗОВАНИЯ

### Шаг 1: Регистрация
```bash
TOKEN=$(curl -s -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "username": "testuser",
    "password": "password123"
  }' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

echo "Token: $TOKEN"
```

### Шаг 2: Получение информации о себе
```bash
curl -X GET http://localhost:8000/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### Шаг 3: Создание видео
```bash
VIDEO_ID=$(curl -s -X POST http://localhost:8000/api/v1/videos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Тестовое видео",
    "description": "Описание",
    "videoUrl": "https://example.com/video.mp4",
    "thumbnailUrl": "https://example.com/thumb.jpg",
    "durationSeconds": 30,
    "hashtags": ["тест", "видео"]
  }' | grep -o '"id":"[^"]*' | cut -d'"' -f4)

echo "Video ID: $VIDEO_ID"
```

### Шаг 4: Создание папки
```bash
FOLDER_ID=$(curl -s -X POST http://localhost:8000/api/v1/folders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Тестовая папка",
    "description": "Описание папки",
    "preference": {
      "allowedHashtags": ["тест"],
      "freshnessWeight": 0.5,
      "popularityWeight": 0.5
    }
  }' | grep -o '"id":"[^"]*' | cut -d'"' -f4)

echo "Folder ID: $FOLDER_ID"
```

### Шаг 5: Лайк на видео
```bash
curl -X POST http://localhost:8000/api/v1/videos/$VIDEO_ID/reactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reactionType": "LIKE"
  }'
```

### Шаг 6: Получение ленты папки
```bash
curl -X GET "http://localhost:8000/api/v1/folders/$FOLDER_ID/feed?limit=10" \
  -H "Authorization: Bearer $TOKEN"
```

---

## ПРИМЕЧАНИЯ

1. **JWT токен** нужно сохранять и использовать во всех запросах (кроме регистрации и входа)
2. **UUID** для видео и папок нужно сохранять из ответов для последующих запросов
3. **Роли:** 
   - `ROLE_USER` - обычный пользователь (может создавать видео, папки, реакции)
   - `ROLE_CREATOR` - создатель контента (дополнительные права на видео)
   - `ROLE_ADMIN` - администратор (доступ к модерации)
4. **Статусы видео:** `DRAFT`, `PENDING`, `PUBLISHED`, `REJECTED`, `DELETED`
5. **Типы реакций:** `LIKE`, `DISLIKE`, `VIEW`, `SHARE`, `REPORT`, `SKIP`

---

## ОБРАБОТКА ОШИБОК

Все ошибки возвращаются в формате:
```json
{
  "timestamp": "2025-12-07T02:53:23.927+03:00",
  "message": "Описание ошибки"
}
```

**HTTP статусы:**
- `200` - OK
- `201` - Created
- `204` - No Content
- `400` - Bad Request (валидация)
- `401` - Unauthorized (нет токена или токен невалидный)
- `403` - Forbidden (нет прав)
- `404` - Not Found
- `500` - Internal Server Error


