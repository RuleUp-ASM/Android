package com.ruleup.core.datastore.di;

import android.content.Context;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DataStoreProvidesModule_ProvideAuthPreferencesDataStoreFactory implements Factory<DataStore<Preferences>> {
  private final Provider<Context> contextProvider;

  private DataStoreProvidesModule_ProvideAuthPreferencesDataStoreFactory(
      Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DataStore<Preferences> get() {
    return provideAuthPreferencesDataStore(contextProvider.get());
  }

  public static DataStoreProvidesModule_ProvideAuthPreferencesDataStoreFactory create(
      Provider<Context> contextProvider) {
    return new DataStoreProvidesModule_ProvideAuthPreferencesDataStoreFactory(contextProvider);
  }

  public static DataStore<Preferences> provideAuthPreferencesDataStore(Context context) {
    return Preconditions.checkNotNullFromProvides(DataStoreProvidesModule.INSTANCE.provideAuthPreferencesDataStore(context));
  }
}
