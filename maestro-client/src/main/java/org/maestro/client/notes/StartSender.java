/*
 *  Copyright 2017 Otavio R. Piske <angusyoung@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.maestro.client.notes;

import org.maestro.common.client.notes.MaestroCommand;

public class StartSender extends MaestroRequest<MaestroSenderEventListener> {
    public StartSender() {
        super(MaestroCommand.MAESTRO_NOTE_START_SENDER);
    }

    @Override
    public void notify(MaestroSenderEventListener visitor) {
        visitor.handle(this);
    }

    @Override
    public String toString() {
        return "StartSender{} " + super.toString();
    }
}
