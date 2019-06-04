/*
 * This file is part of Hawk Anticheat.
 * Copyright (C) 2018 Hawk Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.check.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.Pair;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntity;

import java.util.Map;
import java.util.UUID;

/**
 * The AntiVelocityB check monitors the abuse of an exploit
 * in the client which alters received knockback significantly.
 * The exploit is as follows: sprint-jump on the same tick that you
 * receive knockback. This is difficult to achieve consistently
 * without external assistance; those that can do it consistently
 * are most likely cheating.
 */
public class AntiVelocityB extends MovementCheck implements Cancelless {

    private int SAMPLES;
    private double RATIO_THRESHOLD;

    private Map<UUID, Pair<Integer, Integer>> ratioMap;
    private Map<UUID, Long> landingTickMap;

    public AntiVelocityB() {
        super("antivelocityb", false, -1, 1, 0.9, 5000, "%player% may be using anti-velocity. VL: %vl%", null);
        SAMPLES = (int)customSetting("samples", "", 10);
        RATIO_THRESHOLD = (double)customSetting("ratioThreshold", "", 0.9);
    }

    @Override
    protected void check(MoveEvent event) {
        HawkPlayer pp = event.getHawkPlayer();
        if(event.hasAcceptedKnockback() && pp.isOnGround() && pp.isSprinting()) { //TODO and must have been on ground for at least a couple of ticks (to stop false flaggers)

            Pair<Integer, Integer> ratio = ratioMap.getOrDefault(pp.getUuid(), new Pair<>(0, 0));

            if(event.hasJumped()) { //TODO Test kbs with a Y of 0.42 or if player is under a block. This should eval to FALSE in those cases
                ratio.setKey(ratio.getKey() + 1);
            }
            ratio.setValue(ratio.getValue() + 1);

            if(ratio.getValue() >= SAMPLES) {
                double ratioValue = (double)ratio.getKey() / ratio.getValue();
                if(ratioValue > RATIO_THRESHOLD) {
                    punish(pp, false, event);
                }
                else {
                    reward(pp);
                }
                ratio.setKey(0);
                ratio.setValue(0);
            }

            ratioMap.put(pp.getUuid(), ratio);
        }
    }
}
