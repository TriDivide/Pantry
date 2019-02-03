package com.tridivide.pantry

import java.lang.Exception

interface IDataModelResult<T> {
    fun onComplete(data: T?, exception: Exception?)
}