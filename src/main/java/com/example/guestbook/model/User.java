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

import com.example.guestbook.model.userAssign.AttributesType;
import com.example.guestbook.model.userAssign.EntryType;
import com.example.guestbook.model.userAssign.UserType;
import com.googlecode.objectify.Key;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;


/**
 * A user associated with a subscription
 * TODO: remove public on fields
 **/
@Entity
@Data
@NoArgsConstructor
public class User {
 
@Id 
  public Long id;
  public Person person;
  public Map <String,String> attributes = new HashMap<>(); 
  
  public User(UserType user) {
	  person = new Person(user);
	  attributes = new HashMap<String,String>();
	  for (EntryType et : user.getAttributes().getEntry())
	  {
		  attributes.put(et.getKey(), et.getValue());
	  }
	
	}
}
