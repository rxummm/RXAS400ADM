package com.rxas400adm.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private List<T> records;

    public <R> PageResult<R> map(Function<T, R> mapper) {
        PageResult<R> result = new PageResult<>();
        result.setTotal(this.total);
        result.setRecords(this.records == null ? null : this.records.stream().map(mapper).toList());
        return result;
    }
}