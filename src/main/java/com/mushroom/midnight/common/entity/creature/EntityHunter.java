package com.mushroom.midnight.common.entity.creature;

import com.mushroom.midnight.common.entity.NavigatorFlying;
import com.mushroom.midnight.common.entity.task.EntityTaskHunterIdle;
import com.mushroom.midnight.common.util.MeanValueRecorder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityFlying;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityHunter extends EntityMob implements EntityFlying {
    public float roll;
    public float prevRoll;

    private final MeanValueRecorder deltaYaw = new MeanValueRecorder(20);

    public EntityHunter(World world) {
        super(world);
        this.setSize(1.5F, 1.5F);
        this.moveHelper = new MoveHelper(this);
        this.lookHelper = new LookHelper(this);
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        NavigatorFlying navigator = new NavigatorFlying(this, world);
        navigator.setCanOpenDoors(false);
        navigator.setCanFloat(false);
        navigator.setCanEnterDoors(false);
        return navigator;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.FLYING_SPEED);
        this.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).setBaseValue(0.08);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(100, new EntityTaskHunterIdle(this, 0.6));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.world.isRemote) {
            this.deltaYaw.record(this.rotationYaw - this.prevRotationYaw);
            float deltaYaw = this.deltaYaw.computeMean();

            this.prevRoll = this.roll;
            this.roll = MathHelper.clamp(-deltaYaw * 8.0F, -45.0F, 45.0F);
        } else {
            /*Path path = this.getNavigator().getPath();
            if (path != null) {
                for (int i = 0; i < path.getCurrentPathLength(); i++) {
                    PathPoint point = path.getPathPointFromIndex(i);
                    WorldServer worldServer = (WorldServer) this.world;
                    worldServer.spawnParticle(EnumParticleTypes.REDSTONE, true, point.x + 0.5, point.y + 0.5, point.z + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }*/
        }
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    protected void updateFallState(double y, boolean grounded, IBlockState state, BlockPos pos) {
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (this.isServerWorld() || this.canPassengerSteer()) {
            double speed = this.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).getAttributeValue() * 0.2;

            Vec3d lookVector = this.getLookVec();
            Vec3d moveVector = lookVector.normalize().scale(speed);

            this.motionX += moveVector.x;
            this.motionY += moveVector.y;
            this.motionZ += moveVector.z;

            this.motionX *= 0.91;
            this.motionY *= 0.91;
            this.motionZ *= 0.91;

            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        }

        this.updateLimbs();
    }

    private void updateLimbs() {
        this.prevLimbSwingAmount = this.limbSwingAmount;

        double deltaX = this.posX - this.prevPosX;
        double deltaY = this.posY - this.prevPosY;
        double deltaZ = this.posZ - this.prevPosZ;

        float distance = MathHelper.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        float moveAmount = Math.min(distance * 4.0F, 1.0F);

        this.limbSwingAmount += (moveAmount - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

    private static class MoveHelper extends EntityMoveHelper {
        private static final float CLOSE_TURN_SPEED = 90.0F;
        private static final float FAR_TURN_SPEED = 4.0F;

        private static final float CLOSE_TURN_DISTANCE = 2.0F;
        private static final float FAR_TURN_DISTANCE = 7.0F;

        MoveHelper(EntityHunter parent) {
            super(parent);
        }

        @Override
        public void onUpdateMoveHelper() {
            if (this.action == EntityMoveHelper.Action.MOVE_TO) {
                this.action = EntityMoveHelper.Action.WAIT;

                double deltaX = this.posX - this.entity.posX;
                double deltaZ = this.posZ - this.entity.posZ;
                double deltaY = this.posY - this.entity.posY;
                double distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

                if (distanceSquared < 2.5) {
                    this.entity.setMoveForward(0.0F);
                    return;
                }

                float distance = MathHelper.sqrt(distanceSquared);
                float turnSpeed = this.computeTurnSpeed(distance);

                float targetYaw = (float) (Math.toDegrees(MathHelper.atan2(deltaZ, deltaX))) - 90.0F;
                this.entity.rotationYaw = this.limitAngle(this.entity.rotationYaw, targetYaw, turnSpeed);

                double deltaHorizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                float targetPitch = (float) -Math.toDegrees(Math.atan2(deltaY, deltaHorizontal));
                this.entity.rotationPitch = this.limitAngle(this.entity.rotationPitch, targetPitch, turnSpeed);
            } else {
                this.entity.setMoveForward(0.0F);
            }
        }

        private float computeTurnSpeed(float distance) {
            float lerpRange = FAR_TURN_DISTANCE - CLOSE_TURN_DISTANCE;
            float alpha = MathHelper.clamp((distance - CLOSE_TURN_DISTANCE) / lerpRange, 0.0F, 1.0F);
            return CLOSE_TURN_SPEED + (FAR_TURN_SPEED - CLOSE_TURN_SPEED) * alpha;
        }
    }

    private static class LookHelper extends EntityLookHelper {
        private final EntityLiving parent;

        LookHelper(EntityLiving parent) {
            super(parent);
            this.parent = parent;
        }

        @Override
        public void onUpdateLook() {
            float deltaYaw = MathHelper.wrapDegrees(this.parent.rotationYawHead - this.parent.renderYawOffset);
            if (!this.parent.getNavigator().noPath()) {
                if (deltaYaw < -75.0F) {
                    this.parent.rotationYawHead = this.parent.renderYawOffset - 75.0F;
                }
                if (deltaYaw > 75.0F) {
                    this.parent.rotationYawHead = this.parent.renderYawOffset + 75.0F;
                }
            }
        }
    }
}