/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.gameontext.player.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Player account information")
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public class PlayerArgument {

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;

    /** Player account/record id */
    @JsonProperty("_id")
    @ApiModelProperty(
            value = "Unique player id",
            readOnly = true,
            name = "_id",
            example = "oauthProvider:userid",
            required = true)
    protected String id;

    /** Document revision */
    @JsonProperty("_rev")
    @ApiModelProperty(hidden = true)
    protected String rev;

    @ApiModelProperty(
            value = "Player name",
            example = "Harriet",
            required = true)
    protected String name;
        
    @ApiModelProperty(
            value = "Favorite color",
            example = "Tangerine",
            required = true)
    protected String favoriteColor;

    @JsonCreator
    public PlayerArgument() {}

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getRev() {
        return rev;
    }
    public void setRev(String rev) {
        this.rev = rev;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFavoriteColor() {
        return favoriteColor;
    }

    public void setFavoriteColor(String favoriteColor) {
        this.favoriteColor = favoriteColor;
    }

    @Override
    public String toString() {
        return "PlayerArgument [id=" + id + ", revision=" + rev +", name=" + name 
                + ", favoriteColor=" + favoriteColor + "]";
    }
}
