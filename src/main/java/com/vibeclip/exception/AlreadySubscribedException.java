package com.vibeclip.exception;

import java.util.UUID;

public class AlreadySubscribedException extends ApplicationException {

    public AlreadySubscribedException(UUID targetId) {
        super("Вы уже подписаны на пользователя: " + targetId);
    }
}