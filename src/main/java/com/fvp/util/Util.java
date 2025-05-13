package com.fvp.util;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class Util {

  public List<String> tokenize(String input) {
    try {
      Gson gson = new Gson();
      Type listType = new TypeToken<List<String>>() {
      }.getType();
      return gson.fromJson(input, listType);
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }
}
