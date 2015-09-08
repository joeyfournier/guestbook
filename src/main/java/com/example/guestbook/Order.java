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

package com.example.guestbook;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.googlecode.objectify.Key;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;


/**
 * A person associated with either a creator or user, possibly elsewhere
 * TODO: remove public on fields
 **/
@Entity
@Data
@NoArgsConstructor
public class Order {
  @Id 
  public Long id;
  public String editionCode;
  public String pricingDuration;
  public Map <String,String> orderItems = new HashMap<>(); // assume order items are integer values, may need to change this if non-numeric or decimal found


}
