package dev.strubbelkopp.puyoapple.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class PattingGoal extends Goal {

    private final AppleEntity mob;
    private Entity target;
    private final TargetPredicate targetPredicate;

    public PattingGoal(AppleEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        this.targetPredicate = TargetPredicate.createNonAttackable().setBaseMaxDistance(AppleEntity.HEADPAT_RANGE);
    }

    @Override
    public boolean canStart() {
        if (this.mob.isBeingHeadpatted()) {
            if (this.mob.getTarget() != null) {
                this.target = this.mob.getTarget();
            }
            this.target = this.mob.getWorld().getClosestPlayer(this.targetPredicate, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());

            return this.target != null;
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return this.target.isAlive() && this.mob.isBeingHeadpatted();
    }

    @Override
    public void tick() {
        if (this.target.isAlive()) {
            this.mob.getNavigation().stop();
            this.mob.getLookControl().lookAt(this.target.getX(), this.mob.getEyeY(), this.target.getZ());
        }
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.target = null;
    }
}
