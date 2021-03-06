/*
 * Copyright 2014 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.juzu.tutorial.juzcret.step7.models;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;


public class Comment extends Model {

  private static final long serialVersionUID = -2678463117693193937L;

  private String            userId;
  
  @Pattern(regexp = "^.+$", message = "Comment content must not be empty")
  @NotNull(message = "Comment content is required")
  private String            content;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
