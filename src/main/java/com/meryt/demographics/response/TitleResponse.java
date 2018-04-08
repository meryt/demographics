package com.meryt.demographics.response;

import lombok.NonNull;

import com.meryt.demographics.domain.title.Title;

public class TitleResponse extends TitleReference {

    public TitleResponse(@NonNull Title title) {
        super(title);
    }

}

