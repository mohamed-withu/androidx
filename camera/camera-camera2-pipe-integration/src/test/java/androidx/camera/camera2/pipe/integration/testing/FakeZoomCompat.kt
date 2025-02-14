/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.testing

import android.graphics.Rect
import androidx.camera.camera2.pipe.integration.compat.ZoomCompat
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera

class FakeZoomCompat constructor(
    override val minZoomRatio: Float = 0f,
    override val maxZoomRatio: Float = 0f,
    var croppedSensorArea: Rect = Rect(0, 0, 640, 480),
) : ZoomCompat {
    var zoomRatio = 0f

    override fun apply(zoomRatio: Float, camera: UseCaseCamera) {
        this.zoomRatio = zoomRatio
    }

    override fun getCropSensorRegion() = croppedSensorArea
}
