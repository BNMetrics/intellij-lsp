package org.jetbrains.plugins.scala.lang.psi.adapters;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;

/**
 * Nikolay.Tropin
 * 23-Aug-17
 */

//This interface is required because it's impossible to implement
//overloaded method with different array return types in scala.
public interface PsiModifierListOwnerAdapter extends PsiModifierListOwner, PsiAnnotatedAdapter {
    @NotNull
    @Override
    default PsiAnnotation[] getAnnotations() {
        return psiAnnotations();
    }
}
