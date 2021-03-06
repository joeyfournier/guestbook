/**
 * Copyright 2014-2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.guestbook.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.example.guestbook.model.subOrder.CreatorType;
import com.example.guestbook.model.userAssign.UserType;
import com.googlecode.objectify.Key;

import java.lang.String;


/**
 * A person associated with either a creator or user, possibly elsewhere
 * TODO: remove public on fields
 **/
@Entity
@Data
@NoArgsConstructor
public class Person {
 
  @Id 
  public Long id;
  @Index
  public String email;
  public String firstName;
  public String lastName;
  @Index
  public String openId;
  @Index
  public String uuid;
  public String language; // may be null

  public Person(CreatorType creator) {
		id = null;
		email = creator.getEmail();
		firstName = creator.getFirstName();
		lastName= creator.getLastName();
		openId = creator.getOpenId();
		uuid = creator.getUuid();
		language = creator.getLanguage();
	}

  public Person(UserType user) {
		id = null;
		email = user.getEmail();
		firstName = user.getFirstName();
		lastName= user.getLastName();
		openId = user.getOpenId();
		uuid = user.getUuid();
		language = user.getLanguage();
	}

}
