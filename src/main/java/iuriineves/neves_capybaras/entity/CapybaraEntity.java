package iuriineves.neves_capybaras.entity;

import com.google.common.collect.ImmutableList;
import iuriineves.neves_capybaras.NevesCapybaras;
import iuriineves.neves_capybaras.init.ModEntities;
import iuriineves.neves_capybaras.init.ModItems;
import iuriineves.neves_capybaras.init.ModSoundEvents;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Objects;

public class CapybaraEntity extends AnimalEntity implements GeoEntity {

    public static final List<Type> NATURAL_TYPES = ImmutableList.of(Type.BROWN, Type.DARK, Type.RED);

    public static final TrackedData<Boolean> MANDARIN = DataTracker.registerData(CapybaraEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<String> TYPE = DataTracker.registerData(CapybaraEntity.class, TrackedDataHandlerRegistry.STRING);

    protected final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    Type getRandomNaturalType(Random random) {
        return NATURAL_TYPES.get(random.nextInt(NATURAL_TYPES.size()));
    }

    public Identifier getCapybaraTexture() {
        return this.getCapybaraType().capybaraTexture;
    }

    public Type getCapybaraType() {
        return Type.byName(this.dataTracker.get(TYPE));
    }

    public void setCapybaraType(Type type) {
        this.dataTracker.set(TYPE, type.toString());
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();

        this.dataTracker.startTracking(MANDARIN, false);
        this.dataTracker.startTracking(TYPE, "");
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        nbt.putBoolean("Mandarin", this.hasMandarin());
        nbt.putString("CapybaraType", this.getCapybaraType().toString());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        NevesCapybaras.LOGGER.info(String.valueOf(Type.valueOf(nbt.getString("CapybaraType"))));

        if (nbt.contains("Mandarin")) {
            this.setMandarin(nbt.getBoolean("Mandarin"));
        }

        if (nbt.contains("CapybaraType")) {
            this.setCapybaraType(Type.byName(nbt.getString("CapybaraType")));
        }
    }

    public CapybaraEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {

        if (world.getRandom().nextInt(100) == 0) {
            this.setCapybaraType(Type.ALBINO);
        } else {
            this.setCapybaraType(getRandomNaturalType(world.getRandom()));
        }


        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    public static DefaultAttributeContainer.Builder createCapybaraAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0D).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2f).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.ENTITY_CAPYBARA_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.ENTITY_CAPYBARA_ATTACKED;
    }

    protected SoundEvent getDeathSound() {
        return ModSoundEvents.ENTITY_CAPYBARA_ATTACKED;
    }

    @Override
    public float getMovementSpeed() {
        return super.getMovementSpeed();
    }

    @Override
    public void travel(Vec3d movementInput) {
        // increase speed if on water
        if (this.isTouchingWater()) {
            this.updateVelocity(0.1f, movementInput);
        }
        super.travel(movementInput);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.25));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(4, new TemptGoal(this, 1.2, Ingredient.ofItems(Items.MELON_SLICE), false));
        this.goalSelector.add(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.add(6, new MoveIntoWaterGoal(this));
        this.goalSelector.add(6, new WanderAroundGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {

        //mandarin on head logic
        if (hasMandarin() && player.isSneaking()) {
            if (player.getWorld().isClient()) player.swingHand(Hand.MAIN_HAND);

            if (!player.getWorld().isClient()) {
                if (!player.isCreative())
                    player.getWorld().spawnEntity(new ItemEntity(player.getWorld(), this.getX(), this.getY() + 1, this.getZ(), new ItemStack(ModItems.MANDARIN)));
                setMandarin(false);
            }
       } else if (!hasMandarin() && !this.isBaby()){
            if (hand == Hand.MAIN_HAND && Objects.equals(player.getMainHandStack().getItem(), ModItems.MANDARIN) && !player.isSneaking()) {
                if (player.getWorld().isClient()) player.swingHand(Hand.MAIN_HAND);

                if (!player.getWorld().isClient()) {
                    if (!player.isCreative()) player.getMainHandStack().decrement(1);
                    setMandarin(true);
                }

            }
        }

        return super.interactAt(player, hitPos, hand);
    }

    public void setMandarin(boolean mandarin) {

        NevesCapybaras.LOGGER.info(this.getCapybaraType().toString());
        this.dataTracker.set(MANDARIN, mandarin);
    }

    public boolean hasMandarin() {
        return this.dataTracker.get(MANDARIN);
    }

    private
    <E extends CapybaraEntity>PlayState walkAnimController(final AnimationState<E> event) {
        if (event.isMoving()) {
            event.getController().setAnimationSpeed(1.5);
            return event.setAndContinue(WALK_ANIM);
        }
        event.getController().setAnimationSpeed(1);
        return event.setAndContinue(IDLE_ANIM);
    }

    @Override
    public double getSwimHeight() {
        return 0.7D;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Walking", 0, this::walkAnimController));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Nullable
    @Override
    public CapybaraEntity createChild(ServerWorld world, PassiveEntity entity) {
        CapybaraEntity capybaraEntity = ModEntities.CAPYBARA.create(world);
        assert capybaraEntity != null;
        capybaraEntity.setCapybaraType(this.getCapybaraType());
        return capybaraEntity;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem() == Items.MELON_SLICE;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return super.interactMob(player, hand);
    }

    public enum Type {
        RED(new Identifier(NevesCapybaras.MOD_ID, "textures/entity/red_capybara.png")),
        ALBINO(new Identifier(NevesCapybaras.MOD_ID, "textures/entity/albino_capybara.png")),
        BROWN(new Identifier(NevesCapybaras.MOD_ID, "textures/entity/brown_capybara.png")),
        DARK(new Identifier(NevesCapybaras.MOD_ID, "textures/entity/dark_capybara.png"));

        public final Identifier capybaraTexture;
        Type(Identifier capybaraTexture) {
            this.capybaraTexture = capybaraTexture;
        }

        public static Type byName(String name) {
            for (Type type : values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }
            return Type.RED;
        }
    }
}