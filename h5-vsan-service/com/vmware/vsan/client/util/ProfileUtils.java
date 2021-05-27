package com.vmware.vsan.client.util;

import com.vmware.vim.binding.pbm.profile.Profile;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class ProfileUtils {
   public static Map getPoliciesIdNamePairs(Profile[] profiles) {
      return (Map)Arrays.stream(profiles).collect(Collectors.toMap((profile) -> {
         return profile.profileId.uniqueId;
      }, (profile) -> {
         return profile.name;
      }));
   }

   public static Profile findProfileById(Profile[] profiles, String profileId) {
      return (Profile)Arrays.stream(profiles).filter((profile) -> {
         return profile.profileId.uniqueId.equals(profileId);
      }).findFirst().orElse((Object)null);
   }
}
