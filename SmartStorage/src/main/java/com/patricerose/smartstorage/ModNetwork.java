package com.patricerose.smartstorage;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel.VersionTest;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Sends the search box text from the player's game to the server. */
public final class ModNetwork {
    private ModNetwork() {}

    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(SmartStorage.MODID, "main"))
            .clientAcceptedVersions(VersionTest.exact(PROTOCOL_VERSION))
            .serverAcceptedVersions(VersionTest.exact(PROTOCOL_VERSION))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel()
                .play()
                    .serverbound()
                        .addMain(SearchPacket.class, SearchPacket.STREAM_CODEC, SearchPacket::handle)
            .build();

    /** Makes sure the channel above is created during startup. */
    public static void init() {}

    public static void sendSearch(String text) {
        CHANNEL.send(new SearchPacket(text), PacketDistributor.SERVER.noArg());
    }

    public record SearchPacket(String text) {
        public static final StreamCodec<RegistryFriendlyByteBuf, SearchPacket> STREAM_CODEC =
                StreamCodec.ofMember(SearchPacket::encode, SearchPacket::decode);

        public static void encode(SearchPacket packet, RegistryFriendlyByteBuf buf) {
            buf.writeUtf(packet.text, 64);
        }

        public static SearchPacket decode(RegistryFriendlyByteBuf buf) {
            return new SearchPacket(buf.readUtf(64));
        }

        public static void handle(SearchPacket packet, CustomPayloadEvent.Context context) {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof TerminalMenu menu) {
                menu.setSearch(packet.text);
            }
        }
    }
}
