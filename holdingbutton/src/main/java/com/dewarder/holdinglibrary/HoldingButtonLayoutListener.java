/*
 * Copyright (C) 2017 Artem Glugovsky
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dewarder.holdinglibrary;

public interface HoldingButtonLayoutListener {

    void onBeforeExpand();

    void onExpand();

    void onBeforeCollapse();

    void onCollapse(boolean isCancel);

    void onOffsetChanged(float offset, boolean isCancel);

    void onClickExpand();

    void onClickCollapse();
}
